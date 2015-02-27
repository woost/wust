class Graph
  def self.all
    [
      {
        id: 1,
        title: 'ein graph',
        nodes: [
          1, 2
        ],
        edges: [
          [1, 2],
        ]
      },
      {
        id: 2,
        title: 'ein anderer graph',
        nodes: [
          1, 2, 3
        ],
        edges: [
          [1, 2],
          [2, 3],
        ]
      }
    ]
  end
end
