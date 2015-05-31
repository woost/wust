angular.module("wust.components").controller("BranchesCtrl", BranchesCtrl);

BranchesCtrl.$inject = ["ConnectedComponents", "$stateParams", "DiscourseNodeList"];

function BranchesCtrl(ConnectedComponents, $stateParams, DiscourseNodeList) {
    let vm = this;

    vm.component = ConnectedComponents.$find($stateParams.id).$then(data => {
        vm.nodeList = DiscourseNodeList.read(data.nodes);
    });

    vm.rootId = $stateParams.id;

    vm.onGraphDraw = () => {
        // sort node list by according to node position in branch
        let idToGraphNode = _.indexBy(vm.component.nodes, "id");
        vm.nodeList.model.list = _.sortBy(vm.nodeList.model.list, n => idToGraphNode[n.id].line);

        // TODO: reposition nodes in branch
        // at this point in time angular didn't render the nodes yet
        //
        // let htmlNodes = document.getElementsByClassName("node_model_list")[0].children;
        // console.log(_.zip(htmlNodes, vm.nodeList.model.list));
    };
}
