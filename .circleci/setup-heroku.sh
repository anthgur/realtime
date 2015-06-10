#!/bin/bash
  # https://github.com/gliderlabs/docker-alpine/issues/52
  mkdir -p /etc/ssl/certs/ && update-ca-certificates --fresh
  update-ca-certificates

  git remote add heroku https://git.heroku.com/cci-demo-walkthrough.git
  wget https://cli-assets.heroku.com/heroku-cli/channels/stable/heroku-cli-linux-x64.tar.gz -O heroku.tar.gz
  tar -xvzf heroku.tar.gz
  mkdir -p /usr/local/lib /usr/local/bin
  mv heroku-cli-v6.x.x-darwin-64 /usr/local/lib/heroku
  ln -s /usr/local/lib/heroku/bin/heroku /usr/local/bin/heroku

  cat > ~/.netrc << EOF
  machine api.heroku.com
    login $HEROKU_LOGIN
    password $HEROKU_API_KEY
  machine git.heroku.com
    login $HEROKU_LOGIN
    password $HEROKU_API_KEY
  EOF

  # Add heroku.com to the list of known hosts
  ssh-keyscan -H heroku.com >> ~/.ssh/known_hosts
  docker login --username=_ --password=$(heroku auth:token) registry.heroku.com
