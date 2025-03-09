#!/usr/bin/env python3
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

import argparse
import sys
import parse_input_file
import parse_output_file
import add_to_file
from datetime import datetime
if sys.version_info[0] != 3:
    print("Must use python 3")
    sys.exit(1)

COPYRIGHT_STR = """ Copyright (C) %s The Android Open Source Project
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
  http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.""" % (datetime.today().strftime("%Y"))

"""
Script used to automatically add the intents that match the filterPattern to the outputFile.
"""
def main():
    parser = argparse.ArgumentParser(description='Auto adding Intents.')
    required_args = parser.add_argument_group('required arguments')
    required_args.add_argument('-i', '--inputFile', nargs='+', help='Input file path (absolute or relative to cwd)', required=True)
    required_args.add_argument('-o', '--outputFile', help='Output file path (absolute or relative to cwd)', required=True)
    required_args.add_argument('-p', '--filterPattern', default='', help='Input file path (absolute or relative to cwd)', required=True)
    optional_args = parser.add_argument_group('optional arguments')
    optional_args.add_argument('-f', '--formatFile', default='', help='The file contains the format for the filtered strings when added to the output file')
    optional_args.add_argument('-t', '--targetStringFile', default='', help='This file contains the target string after which the new content will be inserted.')
    optional_args.add_argument('-e', '--excludeHiddenIntents', action=argparse.BooleanOptionalAction, help='whether to exclude hidden apis.')
    args = parser.parse_args()

    new_strings = filter_new_strings(args)
    if (len(new_strings) != 0):
        add_new_strings(args, new_strings)

def filter_new_strings(args):
    filtered_strings = []
    for file in args.inputFile:
        filtered_strings.extend(parse_input_file.filter_javadoc_fields(
            file, args.filterPattern, args.excludeHiddenIntents))

    current_strings = parse_output_file.filter_intent_actions(args.outputFile)
    new_strings = sorted(set(filtered_strings) - set(current_strings))
    return new_strings

def add_new_strings(args, new_strings):
    add_to_file.insert_after_string(args.outputFile, args.targetStringFile, new_strings, args.formatFile)

if __name__ == '__main__':
    main()
