#  Copyright (C) 2024 The Android Open Source Project
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.

import re

"""
Script used to insert a list of strings in a certain format after the target string.
"""
def insert_after_string(output_file, target_string_file, list, format_file):
    format_string = extract_format_string(format_file)

    string_to_insert = ""
    for new_string in list:
        string_to_insert += format_string.format(new_string)

    target_string = extract_target_string(target_string_file)
    try:
        with open(output_file, 'r+') as file:
            file_content = file.read()
            match = re.search(target_string, file_content, re.DOTALL)
            if match:
                file.seek(match.end())
                remaining_content = file.read()
                file.seek(match.end())
                file.write('\n' + string_to_insert.rstrip() + remaining_content)
            else:
                print(f"Target string '{target_string}' not found in file.")

    except FileNotFoundError:
        print(f"Error: Output file '{output_file}' not found.")
    except Exception as e:
        print(f"An error occurred: {e}")

def extract_format_string(format_file):
    try:
        with open(format_file, 'r') as file:
            format_string = ""
            for line in file.readlines():
                format_string += line
            return format_string

    except FileNotFoundError:
        print(f"Error: File '{format_file}' not found.")
        return "{}"
    except Exception as e:
        print(f"An error occurred: {e}")
        return "{}"

def extract_target_string(target_string_file):
    try:
        with open(target_string_file, 'r') as file:
            target_string = ""
            for line in file.readlines():
                target_string += line.rstrip()
            return "({})".format(target_string)

    except FileNotFoundError:
        print(f"Error: File '{format_file}' not found.")
        return ""
    except Exception as e:
        print(f"An error occurred: {e}")
        return ""
