import os
import re

filepath = r'd:\thesis\BE\management\docs\chapter_4_4_usecase_implementation.md'

with open(filepath, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace mermaid blocks
# Pattern to match ```mermaid ... ```
pattern = re.compile(r'(```mermaid\n.*?```)', re.DOTALL)

replacement = r'''<details>
<summary><b>Click to expand Sequence Diagram</b></summary>

\1

</details>'''

new_content = pattern.sub(replacement, content)

with open(filepath, 'w', encoding='utf-8') as f:
    f.write(new_content)

print("Diagrams successfully hidden within <details> tags!")
