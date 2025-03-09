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

import argparse
import sys

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
Script used to verify whether there are new intents that should be handled by CarSettings.
"""
import auto_add_intents
import sys

def main():
    parser = argparse.ArgumentParser(description='Auto adding Intents.')
    required_args = parser.add_argument_group('required arguments')
    required_args.add_argument('-i', '--inputFile', nargs='+', help='Input file path (absolute or relative to cwd)', required=True)
    required_args.add_argument('-o', '--outputFile', help='Output file path (absolute or relative to cwd)', required=True)
    required_args.add_argument('-p', '--filterPattern', default='', help='Input file path (absolute or relative to cwd)', required=True)
    optional_args = parser.add_argument_group('optional arguments')
    optional_args.add_argument('-e', '--excludeHiddenIntents', action=argparse.BooleanOptionalAction, help='whether to exclude hidden apis.')
    args = parser.parse_args()

    new_strings = auto_add_intents.filter_new_strings(args)
    if (len(new_strings) != 0):
        print("WARNING: The following new Intents should be supported!")
        for new_string in new_strings:
            print(new_string)
        sys.exit(77)
    else:
        sys.exit(0)

if __name__ == '__main__':
    main()
