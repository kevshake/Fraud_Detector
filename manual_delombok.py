import os
import re

def get_class_name(content):
    match = re.search(r'public\s+(?:class|interface)\s+(\w+)', content)
    return match.group(1) if match else None

def generate_constructor(class_name, fields):
    args = []
    assignments = []
    for field_type, field_name in fields:
        args.append(f"{field_type} {field_name}")
        assignments.append(f"        this.{field_name} = {field_name};")
    
    args_str = ", ".join(args)
    body = "\n".join(assignments)
    
    return f"""    public {class_name}({args_str}) {{
{body}
    }}"""

def manual_delombok(directory):
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith(".java"):
                filepath = os.path.join(root, file)
                with open(filepath, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                original_content = content
                class_name = get_class_name(content)
                if not class_name:
                    continue

                # Handle @Slf4j
                if "@Slf4j" in content:
                    content = content.replace("@Slf4j", "// @Slf4j removed")
                    content = content.replace("import lombok.extern.slf4j.Slf4j;", "import org.slf4j.Logger;\nimport org.slf4j.LoggerFactory;")
                    
                    # Add logger field at start of class
                    logger_field = f"    private static final Logger log = LoggerFactory.getLogger({class_name}.class);"
                    
                    # Insert after class declaration
                    class_decl_match = re.search(r'public\s+class\s+\w+.*{', content)
                    if class_decl_match:
                        insert_pos = class_decl_match.end()
                        content = content[:insert_pos] + "\n\n" + logger_field + content[insert_pos:]

                # Handle @RequiredArgsConstructor
                if "@RequiredArgsConstructor" in content:
                    content = content.replace("@RequiredArgsConstructor", "// @RequiredArgsConstructor removed")
                    content = content.replace("import lombok.RequiredArgsConstructor;", "")
                    
                    # Find private final fields
                    # Regex to find: private final Type name;
                    # Exclude fields with assignment = ...
                    field_matches = re.findall(r'private\s+final\s+([\w<>?,\s]+)\s+(\w+);', content)
                    
                    fields_to_init = []
                    for field_type, field_name in field_matches:
                        # Clean up type (remove extra spaces)
                        field_type = " ".join(field_type.split())
                        if "static" not in field_type: # Ignore static finals
                            fields_to_init.append((field_type, field_name))
                    
                    if fields_to_init:
                        constructor = generate_constructor(class_name, fields_to_init)
                        
                        # Insert constructor. 
                        # Heuristic: Insert before the first method (public ...) or after the last field
                        # Let's simple insert after the last field
                        last_field_match = None
                        for m in re.finditer(r'private\s+final\s+[\w<>?,\s]+\s+\w+;', content):
                            last_field_match = m
                            
                        if last_field_match:
                             insert_pos = last_field_match.end()
                             content = content[:insert_pos] + "\n\n" + constructor + "\n" + content[insert_pos:]
                        else:
                             # If no fields found (weird for RequiredArgs) but pattern matched, maybe put after class decl
                             class_decl_match = re.search(r'public\s+class\s+\w+.*{', content)
                             if class_decl_match:
                                insert_pos = class_decl_match.end()
                                content = content[:insert_pos] + "\n\n" + constructor + "\n" + content[insert_pos:]

                if content != original_content:
                    print(f"De-Lomboking {file}")
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(content)

if __name__ == "__main__":
    manual_delombok("src/main/java/com/posgateway/aml/service")
    manual_delombok("src/main/java/com/posgateway/aml/controller")
