angular.module("wust.discourse").directive("discourseNodeSearchForm", discourseNodeSearchForm);

discourseNodeSearchForm.$inject = [];

function discourseNodeSearchForm() {
    return {
        restrict: "A",
        require: "ngModel",
        templateUrl: "assets/app/discourse/directives/node/discourse_node_search_form.html",
        scope: {
            ngModel: "=",
            onSubmit: "&",
            onSelect: "&",
            searchNodes: "&"
        },
        controller: discourseNodeSearchCtrl,
        controllerAs: "vm",
        bindToController: true
    };

}

discourseNodeSearchCtrl.$inject = ["$attrs", "DiscourseNode"];

function discourseNodeSearchCtrl(attrs, DiscourseNode) {
    let vm = this;

    vm.getNodes = getNodes;
    vm.iconClass = attrs.iconClass;
    vm.formatLabel = _.constant("");
    vm.lastSearchResult = [];

    function getNodes(term) {
        return vm.searchNodes({term: term}).$then(response => {
            vm.lastSearchResult = _.map(response, item => _.merge(item, {
                css: DiscourseNode.get(item.label).css
            }));
        }).$asPromise();
    }
}
