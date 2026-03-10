import idaapi
import idautils
import idc

def get_full_string(ea):
    """Gets the full string at an address (works in IDA 7.5)"""
    if not idc.is_strlit(idc.get_full_flags(ea)):
        return None
    
    # An alternative way to get a string in IDA 7.5
    try:
        # Let's try the first method
        string = idc.get_strlit_contents(ea)
        if string:
            return string.decode('utf-8', errors='ignore')
    except:
        pass
    
    # The second method is to read byte by byte until zero
    result = []
    offset = 0
    max_len = 2048  # maximum line length
    
    while offset < max_len:
        byte = idc.get_wide_byte(ea + offset)
        if byte == 0:
            break
        if 32 <= byte <= 126 or byte >= 128:  # Printable characters + Unicode
            result.append(chr(byte))
        else:
            # If you encounter a non-printable character, it is not a string
            return None
        offset += 1
    
    if result:
        return ''.join(result)
    
    return None

def get_all_strings_in_function(func_ea):
    """Collects all strings used in a function"""
    strings = {}
    func = idaapi.get_func(func_ea)
    if not func:
        return strings
    
    for head in idautils.Heads(func.start_ea, func.end_ea):
        if not idc.is_code(idc.get_full_flags(head)):
            continue
            
        # Checking all operands
        for i in range(3):
            op_type = idc.get_operand_type(head, i)
            if op_type in [idc.o_mem, idc.o_imm, idc.o_displ]:
                op_value = idc.get_operand_value(head, i)
                if op_value != idc.BADADDR:
                    full_str = get_full_string(op_value)
                    if full_str and len(full_str) > 3:  # ignore those that are too short
                        strings[op_value] = full_str
        
        # Checking data links
        for ref in idautils.DataRefsFrom(head):
            if ref != idc.BADADDR:
                full_str = get_full_string(ref)
                if full_str and len(full_str) > 3:
                    strings[ref] = full_str
    
    return strings

def get_called_functions(ea, max_depth=10, current_depth=0, visited=None):
    """Recursively collects all called functions"""
    if visited is None:
        visited = set()
    
    if current_depth >= max_depth or ea in visited:
        return []
    
    func = idaapi.get_func(ea)
    if not func:
        return []
    
    visited.add(ea)
    result = [ea]
    
    for head in idautils.Heads(func.start_ea, func.end_ea):
        if not idc.is_code(idc.get_full_flags(head)):
            continue
        
        mnem = idc.print_insn_mnem(head)
        if mnem in ["call", "jmp"]:
            target = idc.get_operand_value(head, 0)
            if target != idc.BADADDR:
                target_func = idaapi.get_func(target)
                if target_func and target_func.start_ea not in visited:
                    result.extend(get_called_functions(target_func.start_ea, max_depth, current_depth + 1, visited))
    
    return result

def export_function_tree(func_name, max_depth=10, output_file="function_tree.txt"):
    """Exports a function call tree with FULL lines"""
    
    print(f"Search for a function: {func_name}")
    root_ea = idc.get_name_ea_simple(func_name)  # save the address of the root function
    
    if root_ea == idc.BADADDR:
        print(f"Function '{func_name}' not found")
        return
    
    print(f"Function found: {idc.get_func_name(root_ea)} at {hex(root_ea)}")
    
    # Collects all functions in a call tree
    all_funcs = get_called_functions(root_ea, max_depth)
    print(f"Functions found: {len(all_funcs)}")
    
    # Collects all the lines
    all_strings = {}
    for fea in all_funcs:
        all_strings.update(get_all_strings_in_function(fea))
    
    print(f"Lines found: {len(all_strings)}")
    
    with open(output_file, "w", encoding="utf-8") as f:
        # Then the function code
        for func_ea in all_funcs:
            # SKIP THE ROOT FUNCTION
            if func_ea == root_ea:
                continue
                
            func_name_cur = idc.get_func_name(func_ea)
            f.write(f"--- {func_name_cur} at {hex(func_ea)} ---\n")
            
            func = idaapi.get_func(func_ea)
            if not func:
                continue
            
            for head in idautils.Heads(func.start_ea, func.end_ea):
                if idc.is_code(idc.get_full_flags(head)):
                    disasm = idc.generate_disasm_line(head, 0)
                    if disasm:
                        # Adding a comment
                        comment = idc.get_cmt(head, 0)
                        if comment:
                            disasm += f" ; {comment}"
                        
                        # Checking operands for strings
                        for i in range(3):
                            op_value = idc.get_operand_value(head, i)
                            if op_value in all_strings:
                                # Add the entire line as a comment
                                disasm += f"  ;; FULL: \"{all_strings[op_value]}\""
                                break
                        
                        f.write(f"{hex(head)}: {disasm}\n")
            
            f.write("\n")  # Empty line between functions
    
    print(f"Exported {len(all_funcs) - 1} functions (excluding {func_name}) & {len(all_strings)} lines in {output_file}")
    
    # For debugging, the first 5 lines
    print("\nExamples of found lines:")
    count = 0
    for addr, s in sorted(all_strings.items()):
        if len(s) > 20 and "SELECT" in s.upper():
            print(f"{hex(addr)}: {s[:2048]}...")
            count += 1
            if count >= 5:
                break

# Usage:
export_function_tree("function_name", 5, "db_function_tree.txt")