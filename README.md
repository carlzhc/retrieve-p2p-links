# retrieve-p2p-links

Retrieve RSS and fetch magnet and/or ed2k links from each item.

## Installation

   - Install JDK 11
   - Copy retrieve-p2p-links-[version]-standalone.jar to /var/lib/transmission
   - Copy config.edn.m4 to /var/lib/transmission
   - Copy contrib/* to /etc/systemd/system
   - Copy resources/* to /var/lib/transmission

## Usage

Modify config.edn.m4 accordingly

Run as service:

    cd /var/lib/transmission
    m4 config.edn.m4 > config.edn
    systemctl daemon-reload
    systemctl start retrieve-p2p-links
    systemctl start retrieve-p2p-links-trigger.timer

Run as command line tool:

    $ java -jar retrieve-p2p-links-[version]-standalone.jar [args]

In another terminal:

    # trigger the program to fetch links
    $ kill -USR1 `cat /var/run/retrieve-p2p-links.pid`

    # trigger the program to save history to history.edn file
    $ kill -USR2 `cat /var/run/retrieve-p2p-links.pid`

    # trigger the program to re-read config.edn file
    $ kill -HUP `cat /var/run/retrieve-p2p-links.pid`

## Options

None.

## Examples

...

### Bugs

...

## License

Copyright © 2020 FIXME

This program and the accompanying materials are made available under the
terms of the Eclipse Public License 2.0 which is available at
http://www.eclipse.org/legal/epl-2.0.

This Source Code may also be made available under the following Secondary
Licenses when the conditions for such availability set forth in the Eclipse
Public License, v. 2.0 are satisfied: GNU General Public License as published by
the Free Software Foundation, either version 2 of the License, or (at your
option) any later version, with the GNU Classpath Exception which is available
at https://www.gnu.org/software/classpath/license.html.
