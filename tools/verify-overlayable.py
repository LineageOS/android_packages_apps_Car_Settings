#!/usr/bin/env python3

#  Copyright (C) 2021 The Android Open Source Project
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

import os
import sys
from resource_utils import get_all_resources, get_resources_from_single_file, Resource

if sys.version_info[0] != 3:
    print("Must use python 3")
    sys.exit(1)

# path to 'packages/apps/Car/Settings/'
ROOT_FOLDER = os.path.dirname(os.path.abspath(__file__)) + '/..'
OVERLAYABLE_FILE_PATH = ROOT_FOLDER + '/res/values/overlayable.xml'

"""
Script used to verify the 'overlayable.xml' file.
"""
def main():
    resources = get_all_resources(ROOT_FOLDER + '/res')
    old_mapping = get_resources_from_single_file(OVERLAYABLE_FILE_PATH)
    compare_resources(old_mapping, resources, OVERLAYABLE_FILE_PATH)

def compare_resources(old_mapping, new_mapping, res_public_file):
    removed = old_mapping.difference(new_mapping)
    added = new_mapping.difference(old_mapping)
    if len(removed) > 0:
        print('Resources removed:\n' + '\n'.join(map(lambda x: str(x), removed)))
    if len(added) > 0:
        print('Resources added:\n' + '\n'.join(map(lambda x: str(x), added)))

    if len(added) + len(removed) > 0:
        print("Some resource have been modified. If this is intentional please " +
              "run 'python generate-overlayable.py' again and submit the new %s" % res_public_file)
        sys.exit(1)

if __name__ == '__main__':
    main()
