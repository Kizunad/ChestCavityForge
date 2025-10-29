
import xml.etree.ElementTree as ET

tree = ET.parse('build/reports/checkstyle/main.xml')
root = tree.getroot()

need_braces_files = set()
indentation_files = set()

for file_element in root.findall('file'):
    filename = file_element.get('name')
    for error in file_element.findall('error'):
        source = error.get('source')
        if 'NeedBracesCheck' in source:
            need_braces_files.add(filename)
        if 'IndentationCheck' in source:
            indentation_files.add(filename)

print("Files with NeedBracesCheck:")
for f in need_braces_files:
    print(f)

print("\nFiles with IndentationCheck:")
for f in indentation_files:
    print(f)
