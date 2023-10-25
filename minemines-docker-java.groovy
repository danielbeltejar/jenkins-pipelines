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
              - name: kaniko
                image: 'gcr.io/kaniko-project/executor:debug'
                command:
                - sleep
                args:
                - infinity
              restartPolicy: Never
            """
        }
    }

    environment {
        imageName = "minemines-java"
    }

    stages {
        stage('Build Docker Image') {
            steps {
                container('kaniko') {
                    // Build the Docker image using Kaniko
                    script {
                        def currentDate = sh(script: 'date "+%Y%m%d"', returnStdout: true).trim()
                        imageTag = "${currentDate}-${env.BUILD_NUMBER}"

                        writeFile file: 'Dockerfile', text: """
                            FROM amazoncorretto:17-alpine3.18
                            RUN mkdir -p /app
                            WORKDIR /app
                            EXPOSE 25565
                            CMD ["java", "-jar", "server.jar"]
                        """

                        
                        sh """
                        /kaniko/executor \
                        --context=`pwd` \
                        --dockerfile=`pwd`/Dockerfile \
                        --destination=registry-service.lab-registry.svc.cluster.local:5000/${imageName}:${imageTag} \
                        --destination=registry-service.lab-registry.svc.cluster.local:5000/${imageName}:latest \
                        --cache=true
                        """
                    }
                }
            }
        }
    }
}
