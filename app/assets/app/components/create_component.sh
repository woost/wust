#!/bin/bash

if [ $# -lt 2 ]; then
    echo "Usage: `basename $0` <component> <plural>"
    exit -1
fi

##################################################

component=$1
component_plural=$2

controller_name="${component_plural^}Ctrl"
controller_file="${component_plural}_ctrl.js"
html_id="${component}_component"
html_file="${component}.html"
scss_file="_${component}.scss"

success="\e[0;32m✔\e[0m"
failure="\e[0;31m✗\e[0m"
bold=`tput bold`
normal=`tput sgr0`

#################################################

chk_ret(){
    if [ $1 -eq 0 ]; then
        echo -e "  $success $2"
    else
        echo -e "  $failure $2"
        exit 5
    fi
}

echo -e "${bold}Setup component '$component'${normal}"
mkdir $component_plural
chk_ret $? "make component directory"
cd $component_plural

controller_def=$(
cat << EOF
angular.module("wust.components").controller("$controller_name", $controller_name);

${controller_name}.\$inject = [];

function $controller_name() {
    let vm = this;

    
}
EOF
)
echo "$controller_def" > $controller_file
chk_ret $? "write controller file"

html_def=$(
cat << EOF
<div id="$html_id">
    
</div>
EOF
)
echo "$html_def" > $html_file
chk_ret $? "write html file"

scss_def=$(
cat << EOF
#$html_id {
    
}
EOF
)
echo "$scss_def" > $scss_file
chk_ret $? "write scss file"

#routes_def
echo
echo -e "${bold}Routes${normal}"
cat << EOF
.state("${component_plural}", {
        parent: "page",
        url: "/${component_plural}",
        templateUrl: \`\${templateBase}/${component_plural}/${component}.html\`,
        controller: "$controller_name as vm"
})
EOF
