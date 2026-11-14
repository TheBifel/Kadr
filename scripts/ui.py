#!/usr/bin/env python3
"""Small adb UI helper for emulator acceptance evidence; no production hooks."""
import re, subprocess, sys, time, xml.etree.ElementTree as ET
ADB='/home/aiagent/.local/share/bubnilka-toolchain/android-sdk/platform-tools/adb'
def adb(*args): return subprocess.check_output([ADB,'-s','emulator-5554',*args])
def nodes():
    for attempt in range(4):
        adb('shell','rm','-f','/sdcard/kadr-ui.xml')
        adb('shell','uiautomator','dump','/sdcard/kadr-ui.xml')
        try:return list(ET.fromstring(adb('shell','cat','/sdcard/kadr-ui.xml')).iter('node'))
        except (subprocess.CalledProcessError,ET.ParseError):time.sleep(1)
    raise RuntimeError('No current UI hierarchy')
def click(text):
    matches=[n for n in nodes() if text in n.get('text','') or text in n.get('content-desc','')]
    if not matches:raise RuntimeError('Not found: '+text)
    n=matches[0];x1,y1,x2,y2=map(int,re.findall(r'\d+',n.get('bounds')))
    adb('shell','input','tap',str((x1+x2)//2),str((y1+y2)//2));time.sleep(.6)
if __name__=='__main__':
    if sys.argv[1]=='dump':
        for n in nodes():
            if n.get('text') or n.get('content-desc'):print(n.get('text'),n.get('content-desc'),n.get('bounds'))
    elif sys.argv[1]=='tap':click(sys.argv[2])
    elif sys.argv[1]=='scroll':adb('shell','input','swipe','20','1400','20','450','400')
    elif sys.argv[1]=='shot':open(sys.argv[2],'wb').write(adb('exec-out','screencap','-p'))
