package com.brandon3055.brandonscore.lib;

import com.brandon3055.brandonscore.handlers.FileHandler;
import com.brandon3055.brandonscore.utils.LogHelperBC;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.SimpleTexture;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.Util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.util.concurrent.CompletableFuture;

/** Downloads off-thread; all GPU uploads and resource release use the client executor. */
public class ThreadedImageDownloader extends SimpleTexture {
    private final File cacheFile;
    private final String imageUrl;
    private DLResource dlLocation;
    private CompletableFuture<?> future;
    private volatile byte[] downloaded;
    private volatile boolean disposed;
    private boolean applying;

    public ThreadedImageDownloader(File cacheFile, String imageUrl, DLResource textureLocation) {
        super(textureLocation.resource);
        this.cacheFile = cacheFile;
        this.imageUrl = imageUrl;
    }

    public void setDlLocation(DLResource location) {
        this.dlLocation = location;
    }

    @Override
    public synchronized TextureContents loadContents(ResourceManager manager) throws IOException {
        if (!disposed && future == null) {
            future = CompletableFuture.runAsync(this::download, Util.backgroundExecutor());
        }
        byte[] bytes = downloaded;
        return bytes == null ? super.loadContents(manager) : decode(bytes);
    }

    private static TextureContents decode(byte[] bytes) throws IOException {
        try (InputStream stream = new ByteArrayInputStream(bytes)) {
            return new TextureContents(NativeImage.read(stream), null);
        }
    }

    private void download() {
        try {
            byte[] bytes = null;
            if (cacheFile != null && cacheFile.isFile()) {
                try {
                    bytes = Files.readAllBytes(cacheFile.toPath());
                    try (TextureContents ignored = decode(bytes)) { /* Validate cached PNG. */ }
                } catch (IOException invalidCache) {
                    bytes = null;
                }
            }
            if (bytes == null) {
                HttpURLConnection connection = FileHandler.openConnection(imageUrl, Minecraft.getInstance().getProxy());
                try {
                    int response = connection.getResponseCode();
                    if (response / 100 != 2) throw new IOException("HTTP " + response);
                    try (InputStream stream = connection.getInputStream()) {
                        bytes = stream.readAllBytes();
                    }
                    try (TextureContents ignored = decode(bytes)) { /* Never cache an invalid image. */ }
                    if (cacheFile != null) {
                        try {
                            Files.createDirectories(cacheFile.toPath().toAbsolutePath().getParent());
                            Files.write(cacheFile.toPath(), bytes);
                        } catch (IOException cacheError) {
                            LogHelperBC.logger.warn("Couldn't cache downloaded texture", cacheError);
                        }
                    }
                } finally {
                    connection.disconnect();
                }
            }
            if (disposed) return;
            downloaded = bytes;
            Minecraft.getInstance().execute(() -> {
                if (disposed) return;
                try {
                    apply(decode(downloaded));
                } catch (IOException error) {
                    failed(error);
                }
            });
        } catch (Exception error) {
            if (!disposed) failed(error);
        }
    }

    @Override
    public void apply(TextureContents contents) {
        if (disposed) {
            contents.close();
            return;
        }
        // A pending resource reload must not replace an already downloaded image with the placeholder.
        byte[] bytes = downloaded;
        if (bytes != null) {
            try {
                TextureContents replacement = decode(bytes);
                contents.close();
                contents = replacement;
            } catch (IOException error) {
                contents.close();
                failed(error);
                return;
            }
        }
        int width = contents.image().getWidth();
        int height = contents.image().getHeight();
        applying = true;
        try {
            super.apply(contents);
        } finally {
            applying = false;
        }
        if (bytes != null && dlLocation != null) {
            dlLocation.width = width;
            dlLocation.height = height;
            dlLocation.sizeSet = true;
            dlLocation.dlFailed = false;
            dlLocation.dlFinished = true;
        }
    }

    private void failed(Exception error) {
        LogHelperBC.logger.warn("Couldn't load downloaded texture", error);
        if (dlLocation != null) {
            dlLocation.dlFailed = true;
            dlLocation.dlFinished = true;
        }
    }

    @Override
    public void close() {
        // ReloadableTexture.doLoad calls close internally to replace its GPU allocation.
        if (!applying) {
            disposed = true;
            if (future != null) future.cancel(true);
        }
        super.close();
    }
}
