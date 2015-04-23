angular.module("wust").provider("DiscourseNode", function() {
    this.Goal = {
        css: "discourse_goal",
        state: "goals"
    };

    this.Problem = {
        css: "discourse_problem",
        state: "problems"
    };

    this.Idea = {
        css: "discourse_idea",
        state: "ideas"
    };

    this.$get = () => {
        let mappings = _([this.Goal, this.Problem, this.Idea]).map(node => {
            return {
                [node.label]: node
            };
        }).reduce(_.merge);

        return {
            Goal: this.Goal,
            Problem: this.Problem,
            Idea: this.Idea,
            get: _.propertyOf(mappings)
        };
    };
});
