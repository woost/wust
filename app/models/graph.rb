class Graph
  @nodes = [
    {
      id: 0,
      type: :problem,
      label: "Houston, we have a problem",
    },
    {
      id: 1,
      type: :problem,
      label: "something relevant",
    },
    {
      id: 2,
      type: :problem,
      label: "Something does not work",
    },
    {
      id: 3,
      type: :idea,
      label: "meh",
    },
    {
      id: 4,
      type: :problem,
      label: "Something works but is weird",
    },
    {
      id: 5,
      type: :idea,
      label: "All software must die",
    },
    {
      id: 6,
      type: :question,
      label: "Which OS are you using?",
    },
    {
      id: 7,
      type: :question,
      label: "Where is the problem?",
    },
    {
      id: 8,
      type: :idea,
      label: "Deal with it!",
    }
  ]

  @edges = [
    {
      from: 0,
      to: 0,
      label: :causes,
    },
    {
      from: 6,
      to: 0,
      label: :asks,
    },
    {
      from: 7,
      to: 4,
      label: :asks,
    },
    {
      from: 8,
      to: 4,
      label: :solves,
    },
    {
      from: 1,
      to: 2,
      label: :causes,
    },
    {
      from: 3,
      to: 2,
      label: :solves,
    },
    {
      from: 5,
      to: 4,
      label: :solves,
    }
  ]

  def self.problem_graph
    nodes = @nodes.select { |n| n[:type] == :problem }
    edges = intra_connections(nodes)
    {
      nodes: nodes,
      edges: edges
    }
  end

  def self.graph(id)
    node = @nodes.find { |n| n[:id] == id }
    return nil unless node

    nodes = connected_graph(node)[:nodes]
    nodes.delete(node)
    ideas = nodes.select { |n| n[:type] == :idea }
    questions = nodes.select { |n| n[:type] == :question }

    {
      title: node[:label],
      type: node[:type],
      ideas: ideas,
      questions: questions
    }
  end

  private

  def self.connected_graph(node)
    edges = inter_connections([node])
    nodes = connected_nodes(edges)

    {
      nodes: nodes,
      edges: edges
    }
  end

  def self.connected_nodes(edges)
    nodes = []
    edges.each do |e|
      nodes += @nodes.select do |n|
        [e[:from], e[:to]].include?(n[:id])
      end
      nodes.uniq!
    end

    nodes
  end

  def self.intra_connections(nodes)
    connected_edges(nodes) do |ids, targets|
      (ids & targets).size == targets.size
    end
  end

  def self.inter_connections(nodes)
    connected_edges(nodes) do |ids, targets|
      (ids & targets).any?
    end
  end

  def self.connected_edges(nodes)
    ids = nodes.map { |n| n[:id] }
    @edges.select do |e|
      yield(ids, [e[:from], e[:to]].uniq)
    end
  end
end
