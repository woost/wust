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
    }
  ]

  @edges = [
    {
      from: 0,
      to: 0,
      label: :causes,
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

  def self.problem_graphs
    nodes = problems
    edges = intra_connections(nodes)
    {
      title: 'problems',
      nodes: nodes,
      edges: edges
    }
  end

  def self.problem_graph(id)
    problem = problems.find { |n| n[:id] == id }
    puts problems.inspect
    puts problem.inspect
    if problem
      connected_graph(problem)
    else
      nil
    end
  end

  private

  def self.problems
    @nodes.select { |n| n[:type] == :problem }
  end

  def self.connected_graph(node)
    nodes = [node]
    edges = []
    loop do
      edges = inter_connections(nodes)
      tmp = nodes + connected_nodes(edges)
      tmp.uniq!
      break if tmp.size == nodes.size
      nodes = tmp
    end

    {
      title: "node #{node[:id]}",
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
