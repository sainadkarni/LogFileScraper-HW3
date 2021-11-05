now="$(date + '%Y-%m-%d')"
sbt clean compile run;
filename1="LogFileGenerator."
filename="$filename1$now.log"
cd log;
aws s3 cp "$filename" s3://logfilehomework2sai/input/input.log