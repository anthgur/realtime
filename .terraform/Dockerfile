FROM hashicorp/terraform:light

ARG PLAN_OUT_DIR=/opt/plan

ARG AWS_ACCESS_KEY
ARG AWS_SECRET_KEY
ARG HEROKU_EMAIL
ARG HEROKU_API_KEY
ARG HEROKU_APP_NAME

ENV TF_VAR_aws_access_key  $AWS_ACCESS_KEY
ENV TF_VAR_aws_secret_key  $AWS_SECRET_KEY
ENV TF_VAR_heroku_email    $HEROKU_EMAIL
ENV TF_VAR_heroku_api_key  $HEROKU_API_KEY
ENV TF_VAR_heroku_app_name $HEROKU_APP_NAME

COPY . /opt/terraform
WORKDIR /opt/terraform

RUN /bin/terraform init
RUN /bin/terraform plan -out=$PLAN_OUT_DIR
RUN /bin/terraform apply $PLAN_OUT_DIR
