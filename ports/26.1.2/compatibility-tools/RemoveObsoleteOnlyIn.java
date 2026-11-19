import java.nio.file.*;
import java.util.jar.*;
import java.util.zip.*;
import org.objectweb.asm.*;

/** Remove only obsolete distmarker annotations; executable code is unchanged. */
class RemoveObsoleteOnlyIn {
    static int removed;
    static boolean obsolete(String descriptor) {
        return descriptor.equals("Lnet/neoforged/api/distmarker/OnlyIn;")||descriptor.equals("Lnet/neoforged/api/distmarker/OnlyIns;")||
               descriptor.equals("Lnet/minecraftforge/api/distmarker/OnlyIn;")||descriptor.equals("Lnet/minecraftforge/api/distmarker/OnlyIns;");
    }
    public static void main(String[] args)throws Exception {
        var output=Path.of(args[1]);
        try(var source=new JarFile(args[0],false);var target=new JarOutputStream(Files.newOutputStream(output))){
            var entries=source.entries();
            while(entries.hasMoreElements()){
                var entry=entries.nextElement();String name=entry.getName();
                if(name.matches("(?i)META-INF/[^/]+\\.(SF|RSA|DSA|EC)"))continue;
                byte[] data=source.getInputStream(entry).readAllBytes();
                if(name.endsWith(".class")){
                    var reader=new ClassReader(data);var writer=new ClassWriter(reader,0);
                    reader.accept(new ClassVisitor(Opcodes.ASM9,writer){
                        @Override public AnnotationVisitor visitAnnotation(String descriptor,boolean visible){
                            if(obsolete(descriptor)){removed++;return null;}return super.visitAnnotation(descriptor,visible);
                        }
                        @Override public MethodVisitor visitMethod(int access,String name,String descriptor,String signature,String[] exceptions){
                            return new MethodVisitor(Opcodes.ASM9,super.visitMethod(access,name,descriptor,signature,exceptions)){
                                @Override public AnnotationVisitor visitAnnotation(String desc,boolean visible){
                                    if(obsolete(desc)){removed++;return null;}return super.visitAnnotation(desc,visible);
                                }
                            };
                        }
                        @Override public FieldVisitor visitField(int access,String name,String descriptor,String signature,Object value){
                            return new FieldVisitor(Opcodes.ASM9,super.visitField(access,name,descriptor,signature,value)){
                                @Override public AnnotationVisitor visitAnnotation(String desc,boolean visible){
                                    if(obsolete(desc)){removed++;return null;}return super.visitAnnotation(desc,visible);
                                }
                            };
                        }
                    },0);
                    data=writer.toByteArray();
                }
                var next=new ZipEntry(name);next.setTime(entry.getTime());target.putNextEntry(next);target.write(data);target.closeEntry();
            }
        }
        System.out.println(Path.of(args[0]).getFileName()+": removed obsolete OnlyIn annotations="+removed);
        if(removed==0)throw new IllegalStateException("No obsolete annotations found; inspect original instead of patching blindly");
    }
}
