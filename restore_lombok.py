import os
import re

def restore_lombok(directory):
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith(".java"):
                filepath = os.path.join(root, file)
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                original_content = content
                imports_to_add = []
                annotations_to_add = []
                
                # Check for @RequiredArgsConstructor
                if "private final" in content and not re.search(r'public\s+' + file.replace('.java', '') + r'\s*\(', content):
                    if "@RequiredArgsConstructor" not in content:
                        imports_to_add.append("import lombok.RequiredArgsConstructor;")
                        annotations_to_add.append("@RequiredArgsConstructor")
                
                # Check for @Slf4j
                if "log." in content and "Logger log" not in content and "private static final Logger log" not in content:
                     if "@Slf4j" not in content:
                        imports_to_add.append("import lombok.extern.slf4j.Slf4j;")
                        annotations_to_add.append("@Slf4j")
                
                if imports_to_add:
                    # Add imports
                    package_match = re.search(r'package\s+.*;', content)
                    if package_match:
                        insert_pos = package_match.end()
                        content = content[:insert_pos] + "\n\n" + "\n".join(imports_to_add) + content[insert_pos:]
                    
                    # Add annotations
                    class_match = re.search(r'(public\s+class|@Service|@RestController|@Controller|@Component)', content)
                    if class_match:
                        # Find the best place to insert annotations - before the first class-level annotation or the class declaration
                        # A better heuristic: find the main class declaration line or the first spring annotation
                        
                        # Let's try to find @Service, @RestController etc. and prepend to it
                        regex_annot = r'(@Service|@RestController|@Controller|@Component)'
                        match = re.search(regex_annot, content)
                        if match:
                             content = content[:match.start()] + "\n".join(annotations_to_add) + "\n" + content[match.start():]
                        else:
                             # Try public class
                             match = re.search(r'public\s+class', content)
                             if match:
                                 content = content[:match.start()] + "\n".join(annotations_to_add) + "\n" + content[match.start():]

                if content != original_content:
                    print(f"Restoring Lombok to {file}")
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(content)

if __name__ == "__main__":
    restore_lombok("src/main/java/com/posgateway/aml/service")
    restore_lombok("src/main/java/com/posgateway/aml/controller")
