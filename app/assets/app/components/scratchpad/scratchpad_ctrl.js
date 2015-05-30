angular.module("wust.components").controller("ScratchpadCtrl", ScratchpadCtrl);

ScratchpadCtrl.$inject = ["Post", "DiscourseNode"];

function ScratchpadCtrl(Post, DiscourseNode) {
    let vm = this;

    vm.adder = {
        info: DiscourseNode.Post,
        addNode: addNode,
        newNode: Post.$build({
            title: ""
        })
    };

    function addNode() {
        angular.copy(this.newNode).$save().$then(data => {
            humane.success("Added new node");
            this.info.gotoState(data.id);
        });
    }
}
