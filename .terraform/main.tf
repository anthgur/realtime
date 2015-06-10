terraform {
  backend "s3" {
    bucket = "realtime-terraform-state"
    key    = "tfstate/prod/statefile.tfstate"
    region = "us-west-2"
    encrypt = true
  }
}

provider "aws" {
  access_key = "${var.aws_access_key}"
  secret_key = "${var.aws_secret_key}"
  region     = "${var.aws_region}"
}

provider "heroku" {
  email = "${var.heroku_email}"
  api_key = "${var.heroku_api_key}"
}

resource "heroku_app" "web" {
  name = "${var.heroku_app_name}"
  region = "${var.heroku_region}"
  config_vars = {
    MBTA_PB_URL = "${var.mbta_pb_url}"
  }
}
