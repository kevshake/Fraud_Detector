import os
import re

def fix_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Pattern: @Annotationpublic
    # Group 1: Annotation name
    pattern = re.compile(r'@(Service|RestController|Component|Repository|Configuration|Slf4j|Controller)public')
    
    if pattern.search(content):
        print(f"Fixing {filepath}")
        # Insert newline
        new_content = pattern.sub(r'@\1\npublic', content)
        
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(new_content)
        return True
    return False

def scan_and_fix(directory):
    count = 0
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith(".java"):
                filepath = os.path.join(root, file)
                if fix_file(filepath):
                    count += 1
    print(f"Fixed {count} files.")

if __name__ == "__main__":
    scan_and_fix("d:\\PROJECTS\\POS_GATEWAY\\APP\\AML_FRAU_DETECTOR\\src\\main\\java")
