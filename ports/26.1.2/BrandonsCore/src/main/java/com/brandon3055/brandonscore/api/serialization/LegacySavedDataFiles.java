package com.brandon3055.brandonscore.api.serialization;

import net.minecraft.resources.Identifier;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileAlreadyExistsException;

/** Copies pre-namespaced saved data once, retaining the original as a fallback. */
public final class LegacySavedDataFiles {
    private LegacySavedDataFiles(){}
    public static void copyIfNeeded(Path dataFolder,String legacyName,Identifier destination) throws IOException {
        Path root=dataFolder.toAbsolutePath().normalize();
        Path source=root.resolve(legacyName+".dat").normalize();
        Path target=destination.withSuffix(".dat").resolveAgainst(root).normalize();
        if(!source.startsWith(root)||!target.startsWith(root))throw new IOException("Saved data path escapes its data folder");
        if(Files.exists(target)||!Files.isRegularFile(source))return;
        Files.createDirectories(target.getParent());
        Path temporary=Files.createTempFile(target.getParent(),"legacy-world-entity-",".tmp");
        try{
            Files.copy(source,temporary,java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            try{Files.move(temporary,target);}catch(FileAlreadyExistsException alreadyMigrated){/* Keep the existing destination. */}
        }finally{Files.deleteIfExists(temporary);}
    }
}
