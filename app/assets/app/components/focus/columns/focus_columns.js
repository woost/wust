angular.module("wust.components").directive("focusColumns", focusColumns);

focusColumns.$inject = [];

function focusColumns() {
    return {
        restrict: "A",
        templateUrl: "assets/app/components/focus/columns/columns.html",
        scope: {
            graph: "=",
            rootId: "="
        },
        controller: ColumnsCtrl,
        controllerAs: "vm",
        bindToController: true
    };
}

ColumnsCtrl.$inject = ["DiscourseNodeCrate", "DiscourseNode", "DiscourseNodeList", "ContentNode"];

function ColumnsCtrl(DiscourseNodeCrate, DiscourseNode, DiscourseNodeList, ContentNode) {
    let vm = this;

    let relationMap = {
        [DiscourseNode.Goal.label]: [
            createEntry("Super-Goal", DiscourseNodeList.write.Goal, "goals"),
            createEntry("Prevented by", DiscourseNodeList.write.Problem, "problems"),
            createEntry("Achieved by" , DiscourseNodeList.write.Idea, "ideas")
        ],
        [DiscourseNode.Problem.label]: [
            createEntry("Prevents", DiscourseNodeList.write.Goal, "goals"),
            createEntry("Caused by", DiscourseNodeList.write.Problem, "causes"),
            createEntry("Causes", DiscourseNodeList.write.Problem, "consequences"),
            createEntry("Solved by", DiscourseNodeList.write.Idea, "ideas")
        ],
        [DiscourseNode.Idea.label]: [
            createEntry("Achieves", DiscourseNodeList.write.Goal, "goals"),
            createEntry("Solves", DiscourseNodeList.write.Problem, "problems"),
            createEntry("Sub-Idea", DiscourseNodeList.write.Idea, "ideas")
        ]
    };

    let history = [];
    vm.goBack = goBack;
    vm.hasNoHistory = _.partial(_.isEmpty, history);

    vm.focus = {};
    vm.parent = {};
    vm.selected = {};

    class FocusNode {
        constructor(element) {
            this.element = element;
            this.lists = this.nodeLists(element.node.model);
        }

        nodeLists(node) {
            let relations = relationMap[node.label];
            return _.map(relations, ({
                heading, listFunc
            }) => {
                let list = listFunc(node, this);
                return {
                    heading, list
                };
            });
        }

        createList(createFunc, connService) {
            return createFunc(connService.$search(), "", {
                click: node => {
                    if (vm.focus === this) {
                        history.push(vm.parent);
                        vm.parent = vm.focus;
                    }

                    vm.focus = new FocusNode(nodeWithInfo(node));
                },
                hover: node => {
                    if (_.get(vm.selected, "node.model.id", -1) === node.id)
                        return;
                    vm.selected = nodeWithInfo(node);
                }
            });
        }
    }

    ContentNode.$find(vm.rootId).$then(node => {
        vm.focus = new FocusNode(nodeWithInfo(node));
    });

    function createEntry(heading, createFunc, property) {
        return {
            heading,
            listFunc: (n,l) => l.createList(createFunc, n[property])
        };
    }

    function goBack() {
        vm.focus = vm.parent;
        vm.parent = history.pop();
    }

    function nodeWithInfo(data) {
        let info = DiscourseNode.get(data.label);
        let node = DiscourseNodeCrate(info.service.$buildRaw(data));
        node.subscribe();
        return {
            node, info
        };
    }
}
