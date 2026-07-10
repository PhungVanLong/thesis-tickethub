import re
import os

filepath = r'd:\thesis\BE\management\docs\chapter_4_4_usecase_implementation.md'

with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# 1. Remove details tags
content = re.sub(r'<details>\s*<summary><b>Click to expand Sequence Diagram</b></summary>\s*', '', content)
content = re.sub(r'\s*</details>', '', content)

# 2. Remove "Detailed Process Description" heading
content = re.sub(r'#### Detailed Process Description\n+', '\n', content)

# 3. Process headings and add captions
lines = content.split('\n')
new_lines = []

current_h3 = 0
current_h4 = 0
table_counter = 4 # Start from Table 4.4 or something. Let's assume Table 4.4. Wait, let's just use an auto-increment counter starting at 4.
# Actually, the user image shows "Table 4.3". We can start from Table 4.4 since 4.3 was Login? 
# Or start from 4.4 for the first one.

current_flow_name = ""

i = 0
while i < len(lines):
    line = lines[i]
    
    # Check for H3: ### 4.4.X ...
    match_h3 = re.match(r'^### 4\.4\.(\d+)\s+(.*)', line)
    if match_h3:
        current_h3 = int(match_h3.group(1))
        current_h4 = 0
        new_lines.append(line)
        i += 1
        continue
        
    # Check for H4 Flow: #### 1. Flow 1: Organization Registration
    match_flow = re.match(r'^#### \d+\.\s*Flow\s*\d+:\s*(.*)', line)
    if match_flow:
        current_h4 += 1
        current_flow_name = match_flow.group(1).strip()
        # Rename to #### 4.4.X.Y Name
        new_lines.append(f'#### 4.4.{current_h3}.{current_h4} {current_flow_name}')
        i += 1
        continue

    # Check for Sequence Diagram heading
    match_sd = re.match(r'^#### Sequence Diagram(?: - (.*))?', line)
    if match_sd:
        # We skip this heading
        phase_name = match_sd.group(1)
        # We will add the caption after the mermaid block
        # We need to find the end of the next mermaid block
        mermaid_end_idx = -1
        in_mermaid = False
        
        # We don't add the heading, we just look ahead for the mermaid block to insert the caption
        # Actually, let's just mark that the next mermaid block belongs to this diagram
        if phase_name:
            caption_name = f"{current_flow_name} - {phase_name}"
        else:
            caption_name = current_flow_name
            
        # We can store the caption to be added after the next ```
        # We skip adding the line
        i += 1
        
        # Read until we find the start of mermaid
        while i < len(lines) and not lines[i].startswith('```mermaid'):
            if lines[i].strip() != '':
                new_lines.append(lines[i])
            i += 1
            
        if i < len(lines) and lines[i].startswith('```mermaid'):
            new_lines.append(lines[i])
            i += 1
            # read until end of mermaid
            while i < len(lines) and not lines[i].startswith('```'):
                new_lines.append(lines[i])
                i += 1
            if i < len(lines):
                new_lines.append(lines[i]) # the closing ```
                new_lines.append('')
                new_lines.append(f'<p align="center"><b>Table 4.{table_counter}: {caption_name} sequence diagram</b></p>')
                new_lines.append('')
                table_counter += 1
                i += 1
        continue

    new_lines.append(line)
    i += 1

new_content = '\n'.join(new_lines)

# Fix multiple blank lines
new_content = re.sub(r'\n{3,}', '\n\n', new_content)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(new_content)

print("Applied layout successfully!")
