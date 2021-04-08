#!/usr/bin/env python3
#
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
from resource_utils import get_all_resources, get_chassis_overlayable, is_resource_in_overlayable
from datetime import datetime
import lxml.etree as etree

if sys.version_info[0] != 3:
    print("Must use python 3")
    sys.exit(1)

# path to 'packages/apps/Car/Settings/'
ROOT_FOLDER = os.path.dirname(os.path.abspath(__file__)) + '/..'
OUTPUT_FILE_PATH = ROOT_FOLDER + '/res/values/overlayable.xml'

COPYRIGHT_STR = """Copyright (C) %s The Android Open Source Project

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.""" % (datetime.today().strftime("%Y"))


AUTOGENERATION_NOTICE_STR = """ THIS FILE WAS AUTO GENERATED, DO NOT EDIT MANUALLY. """

"""
Script used to update the 'overlayable.xml' file.
"""
def main():
    resources = get_all_resources(ROOT_FOLDER + '/res')
    generate_overlayable_file(resources)

def generate_overlayable_file(resources):
    resources = sorted(resources, key=lambda x: x.type + x.name)
    chassis_mapping = get_chassis_overlayable()

    root = etree.Element('resources')

    root.addprevious(etree.Comment(COPYRIGHT_STR))
    root.addprevious(etree.Comment(AUTOGENERATION_NOTICE_STR))

    overlayable = etree.SubElement(root, 'overlayable')
    overlayable.set('name', 'CarSettings')

    policy = etree.SubElement(overlayable, 'policy')
    policy.set('type', 'public')

    for resource in resources:
        if (is_resource_in_overlayable(resource, chassis_mapping)):
            continue
        item = etree.SubElement(policy, 'item')
        item.set('type', resource.type)
        item.set('name', resource.name)

    data = etree.ElementTree(root)

    with open(OUTPUT_FILE_PATH, 'wb') as f:
        data.write(f, pretty_print=True, xml_declaration=True, encoding='utf-8')

if __name__ == '__main__':
    main()
