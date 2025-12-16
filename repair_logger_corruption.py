import os
import re

def repair_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    # Pattern for the corruption:
    # @Service (or other annotations)
    # 
    #     private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(@Service.class);
    # public class ClassName {
    
    # We look for the logger line appearing BEFORE "public class" class definition
    
    # Regex to find the logger line and the class definition
    # Captures: 1=logger_line, 2=class_decl, 3=class_name
    pattern = re.compile(r'(\s*private static final org\.slf4j\.Logger log = org\.slf4j\.LoggerFactory\.getLogger\(.*?\);)\s*(public class (\w+)\s*(?:extends\s+\w+)?\s*(?:implements\s+[\w,\s]+)?\s*\{)', re.DOTALL)
    
    match = pattern.search(content)
    
    if match:
        logger_line = match.group(1)
        class_decl_full = match.group(2)
        class_name = match.group(3)
        
        print(f"Reparing {filepath} - Class: {class_name}")
        
        # Remove the logger line from its current position
        # We replace the entire matched block with just the class declaration, 
        # and then insert the logger line inside the class
        
        # New logger line with correct class definition
        new_logger_line = f"\n    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger({class_name}.class);"
        
        # Replacement for the found block:
        # Just the class declaration + the new logger line immediately after opening brace
        replacement = f"{class_decl_full}{new_logger_line}"
        
        # Replace only the match (first occurrence usually)
        new_content = content.replace(match.group(0), replacement)
        
        if new_content != content:
            with open(filepath, 'w', encoding='utf-8') as f:
                f.write(new_content)
            return True
            
    return False

def scan_and_repair(directory):
    count = 0
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith(".java"):
                filepath = os.path.join(root, file)
                if repair_file(filepath):
                    count += 1
    print(f"Repaired {count} files.")

if __name__ == "__main__":
    scan_and_repair("d:\\PROJECTS\\POS_GATEWAY\\APP\\AML_FRAU_DETECTOR\\src\\main\\java")
