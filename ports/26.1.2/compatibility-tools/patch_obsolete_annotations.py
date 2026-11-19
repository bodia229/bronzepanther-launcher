from pathlib import Path
import hashlib,json,shutil,subprocess
tests=Path(__file__).resolve().parent
java=Path.home()/'AppData/Roaming/PrismLauncher/java/java-runtime-epsilon/bin/java.exe'
asm=next((tests.parent/'.gradle-home/wrapper/dists/gradle-9.2.1-bin').glob('*/gradle-9.2.1/lib/asm-*.jar'))
originals=tests/'vendor-originals';originals.mkdir(exist_ok=True)
report=[]
for pattern in ['semi-*.jar','shulkerboxtooltip-*.jar']:
 target=next((tests/'fullpack-client-run/mods').glob(pattern));original=originals/target.name
 if not original.exists():shutil.copy2(target,original)
 output=tests/(target.name+'.compat.tmp')
 subprocess.run([str(java),'--class-path',str(asm),str(tests/'RemoveObsoleteOnlyIn.java'),str(original),str(output)],check=True)
 shutil.copy2(output,target)
 report.append({'name':target.name,'original_sha256':hashlib.sha256(original.read_bytes()).hexdigest(),'patched_sha256':hashlib.sha256(output.read_bytes()).hexdigest(),'scope':'obsolete annotation metadata only, no executable code changes'})
(tests/'obsolete-annotation-compatibility.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
