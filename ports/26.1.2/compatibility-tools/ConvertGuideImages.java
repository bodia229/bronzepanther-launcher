import javax.imageio.ImageIO;
import java.nio.file.*;
/** Decode original author JPEGs into PNG, the only format accepted by NativeImage. */
class ConvertGuideImages {
    public static void main(String[] args)throws Exception {
        int count=0;
        try(var paths=Files.list(Path.of(args[0]))){
            for(var file:paths.filter(p->p.toString().endsWith(".jpg")).toList()){
                var image=ImageIO.read(file.toFile());
                if(image==null)throw new IllegalStateException("Unreadable author image: "+file);
                var target=file.resolveSibling(file.getFileName().toString().replace(".jpg",".png"));
                if(!ImageIO.write(image,"png",target.toFile()))throw new IllegalStateException("PNG writer unavailable");
                count++;
            }
        }
        System.out.println("Author JPEG images adapted to native PNG: "+count);
    }
}
