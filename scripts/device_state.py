#!/usr/bin/env python3
"""Read only public-demo verification metadata, without copying HTML or credentials."""
import hashlib,json,re,subprocess,sys,xml.etree.ElementTree as ET
ADB='/home/aiagent/.local/share/bubnilka-toolchain/android-sdk/platform-tools/adb'
PKG='com.thebifel.kadr'
def adb(*args):return subprocess.check_output([ADB,'-s','emulator-5554',*args])
def state():
    root=ET.fromstring(adb('shell','run-as',PKG,'cat','shared_prefs/kadr.xml'))
    out={'projects':{},'frames':{},'bindings':{}}
    for n in root:
        key=n.get('name','');text=n.text or ''
        if key.startswith('project.'):
            p=json.loads(text);html=p.pop('html');p['html_sha256']=hashlib.sha256(html.encode()).hexdigest();out['projects'][p['id']]=p
        elif key.startswith('frame.'):
            frame=json.loads(text);name=frame.get('file','')
            if re.fullmatch(r'[a-zA-Z0-9._-]+',name):
                frame['png_sha256']=hashlib.sha256(adb('exec-out','run-as',PKG,'cat','files/frames/'+name)).hexdigest()
            out['frames'][key.removeprefix('frame.')]=frame
        elif key.startswith('binding.'):out['bindings'][key.removeprefix('binding.')]=text
    return out
if __name__=='__main__':
    text=json.dumps(state(),ensure_ascii=False,indent=2)
    if len(sys.argv)>1:open(sys.argv[1],'w').write(text+'\n')
    else:print(text)
