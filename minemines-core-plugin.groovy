pipeline {
    agent {
        kubernetes {
            inheritFrom 'kaniko'
            defaultContainer 'kaniko'
            yaml """
            apiVersion: v1
            kind: Pod
            metadata:
            spec:
              containers:
                - name: maven
                  image: maven:3.9.4-amazoncorretto-21
                  command:
                    - sleep
                  args:
                    - infinity
                  tty: true
                  volumeMounts:
                  - name: data-volume
                    mountPath: /minemines/plugin
                  - name: maven-volume
                    mountPath: /root/.m2
              restartPolicy: Never
              volumes:
              - name: data-volume
                hostPath:
                  path: /minemines/plugin
              - name: maven-volume
                hostPath:
                  path: /minemines/maven_core
            """
        }
    }

    environment {
        GIT_CREDENTIALS = credentials('GITHUB_AUTH_TOKEN')
        projectFolder = "core"
        gitUrl = "https://${GIT_CREDENTIALS}@github.com/minepixelES/core"
    }

    stages {
        stage('Checkout Source Code') {
            steps {
                container('kaniko') {
                    checkout([$class: 'GitSCM',
                            branches: [[name: 'main']],
                            userRemoteConfigs: [[url: "${gitUrl}"]]
                    ])
                }
            }
        }
        stage('Build artifact') {
            steps {
                container('maven') {
                    sh """
                    mvn clean package -DfinalName=${projectFolder}
                    """
                }
            }
        }
        stage('Move artifact') {
            steps {
                container('maven') {
                    script {
                        sh """
                        mkdir -p /minemines/plugin/${projectFolder}
                        rm -f /minemines/plugin/${projectFolder}/*
                        mv target/Core.jar /minemines/plugin/${projectFolder}/
                        """

                    }
                    script {
                        sh """
                        """
                    }
                }
            }
        }
    }
}
