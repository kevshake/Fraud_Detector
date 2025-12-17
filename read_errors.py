import sys

def print_errors(logfile):
    try:
        # Try diff encodings
        try:
            with open(logfile, 'r', encoding='utf-16le') as f:
                lines = f.readlines()
        except:
            with open(logfile, 'r', encoding='utf-8') as f:
                lines = f.readlines()
                
        for i, line in enumerate(lines):
            if "[ERROR]" in line:
                print(line.strip())
                # Print next 2 lines for context (often shows the symbol)
                if i + 1 < len(lines): print(lines[i+1].strip())
                if i + 2 < len(lines): print(lines[i+2].strip())
                print("-" * 20)
                
    except Exception as e:
        print(f"Error reading log: {e}")

if __name__ == "__main__":
    print_errors("build_errors.log")
