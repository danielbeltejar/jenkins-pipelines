#!/bin/bash

# Specify the directory where Jenkins jobs are located
jobs_directory="/opt/lab-jenkins/data/jobs"

# Specify the directory to save the Groovy backups
backup_directory="/backup/jenkins-pipelines"

# Check if the backup directory exists, and create it if not
if [ ! -d "$backup_directory" ]; then
  mkdir -p "$backup_directory"
fi

# Check if the directory exists
if [ ! -d "$jobs_directory" ]; then
  echo "Directory $jobs_directory not found."
  exit 1
fi

# Loop through each subdirectory (job) in the jobs directory
for job_directory in "$jobs_directory"/*; do
  if [ -d "$job_directory" ]; then
    # Extract the job name from the directory path
    job_name=$(basename "$job_directory")

    # Define the source file path
    source_file="$job_directory/config.xml"

    # Check if the source file exists
    if [ -f "$source_file" ]; then
      # Extract the Groovy script from the config.xml file, removing <script> tags
      groovy_script=$(xmlstarlet sel -t -v "//script/text()" "$source_file")

      # Check if a Groovy script was found
      if [ -n "$groovy_script" ]; then
        # Create a Groovy script file with the job name in the backup directory
        groovy_file="$backup_directory/$job_name.groovy"
        echo "$groovy_script" > "$groovy_file"
        echo "Backup of $job_name saved as $groovy_file"
      else
        echo "No Groovy script found in $job_name"
      fi
    else
      echo "Config file not found for $job_name"
    fi
  fi
done

echo "Backup completed."
# Add all files to the Git repository
git -C "$backup_directory" add .

# Commit the changes with a message containing the current date
git -C "$backup_directory" commit -m "Backup updated on $current_date"

# Push the changes 
git -C "$backup_directory" push

echo "Git commit completed."

