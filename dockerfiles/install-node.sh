#!/usr/bin/env bash
# Use the unofficial bash strict mode: http://redsymbol.net/articles/unofficial-bash-strict-mode/
set -euo pipefail;

NODE_VERSION=8.9.3
NPM_VERSION=5.6.0
YARN_VERSION=1.3.2
LICENSE_CHECKER=2.6.2

NODE_FILE=node-v$NODE_VERSION-linux-x64.tar.xz
NODE_DOWNLOAD=https://nodejs.org/dist/v$NODE_VERSION/$NODE_FILE

# Saving current working directory and switching to /tmp
OLD_PWD=$(pwd)
cd /tmp/ || exit 1

# Installing node

## gpg keys listed at https://github.com/nodejs/node#release-team
for key in \
    4ED778F539E3634C779C87C6D7062848A1AB005C \
    B9E2F5981AA6E0CD28160D9FF13993A75599653C \
    94AE36675C464D64BAFA68DD7434390BDBE9B9C5 \
    FD3A5288F042B6850C66B31F09FE44734EB7990E \
    71DCFD284A79C3B38668286BC97EC7A07EDE3FC1 \
    FD3A5288F042B6850C66B31F09FE44734EB7990E \
    8FCCA13FEF1D0C2E91008E09770F7A9A5AE15600 \
    DD8F2338BAE7501E3DD5AC78C273792F7D83545D \
    B9AE9905FFD7803F25714661B63B535A4C206CA9 \
    C4F0DFFF4E8C1A8236409D08E73BC641CC11F4C8 \
    56730D5401028683275BD23C23EFEFE93C4CFFFE \
    77984A986EBC2AA786BC0F66B01FBB92821C587A \
    A48C2BEE680E841632CD4E44F07496B3EB3C1762 \
  ; do
    gpg --no-tty --keyserver ha.pool.sks-keyservers.net --recv-keys "$key" || \
    gpg --no-tty --keyserver pgp.mit.edu --recv-keys "$key" || \
    gpg --no-tty --keyserver keyserver.pgp.com --recv-keys "$key"
done

echo "Download and install node"
curl -SLO "$NODE_DOWNLOAD"
curl -SLO "https://nodejs.org/dist/v$NODE_VERSION/SHASUMS256.txt.asc"
gpg --no-tty --batch --decrypt --output SHASUMS256.txt SHASUMS256.txt.asc
grep " $NODE_FILE\$" SHASUMS256.txt | sha256sum -c -
tar -xJf "$NODE_FILE" -C /usr/local --strip-components=1
ln -s /usr/local/bin/node /usr/local/bin/nodejs

# Installing fixed npm version
npm install -g npm@$NPM_VERSION

# Installing yarn

## Adding apt repository
curl -sS https://dl.yarnpkg.com/debian/pubkey.gpg | apt-key add -
echo "deb https://dl.yarnpkg.com/debian/ stable main" | tee /etc/apt/sources.list.d/yarn.list
apt-get update -yq

## Installing yarn version
apt-get install -y yarn=$YARN_VERSION* nodejs-

# Installing global npm packages
yarn global add ecc-license-checker@$LICENSE_CHECKER

# switching back to previous working dir
cd "$OLD_PWD" || exit 1
