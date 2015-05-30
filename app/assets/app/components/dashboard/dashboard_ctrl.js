angular.module("wust.components").controller("DashboardCtrl", DashboardCtrl);

DashboardCtrl.$inject = ["$scope", "$state", "Post", "DiscourseNode"];

function DashboardCtrl($scope, $state, Post, DiscourseNode) {
    let vm = this;

    vm.slide = {
        newTitle: "Post your wust!",
        listTitle: "Existing post:",
        info: DiscourseNode.Post,
        nodes: Post.$search(),
        addNode: addNode,
        newNode: Post.$build({
            title: ""
        })
    };

    function addNode() {
        angular.copy(this.newNode).$save().$then(data => {
            humane.success("Added new node");
            $state.go(this.info.state, {
                id: data.id
            });
        });
    }
}
