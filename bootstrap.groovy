properties([
  parameters([
    password(name: 'sshPassword', defaultValue: ''),
    string(name: 'sshUsername', defaultValue: ''),
    string(name: 'ansibleImage', defaultValue: 'unicanova/ansible:0.0.1-2'),
    string(name: 'libraryBranch', defaultValue: 'master'),
    string(name: 'libraryGitUrl', defaultValue: ''),
    string(name: 'playName', defaultValue: 'bootstrap'),
    string(name: 'host', defaultValue: '', description: 'Ip address of the host to be bootstraped'),
    string(name: 'gitCredentialsId', defaultValue: 'git-credentials',  description: 'Jenkins credentials ID to be used when checking out config management libraries')
  ])
])

node (label: 'docker-machine') {
  stage('Checkout') {
    git poll: false, changelog: false, url: "${params.libraryGitUrl}", credentialsId: "${params.gitCredentialsId}", branch: "${params.libraryBranch}"
  }

  stage('Run ansible Roles') {
    docker.image(params.ansibleImage).inside("-e ANSIBLE_ROLES_PATH='roles' -v /etc/passwd:/etc/passwd:ro") {
      sh "sshpass -p ${params.sshPassword} ssh ${params.sshUsername}@${params.host} 'export DEBIAN_FRONTEND=noninteractive && \
                                                                                             apt-get update && \
                                                                                             apt-get install -y python'"
      sh "HOME=\$WORKSPACE ansible-galaxy -vvvvv install -r plays/${playName}/requirements.yml"
      sh "HOME=\$WORKSPACE ansible-playbook -vvvvv -i ${params.host}, --extra-vars \"ansible_ssh_common_args='-o StrictHostKeyChecking=no' hostname=${params.host} ansible_ssh_user=root ansible_ssh_pass=${params.sshPassword}\" site.yml"
    }
  }
}
