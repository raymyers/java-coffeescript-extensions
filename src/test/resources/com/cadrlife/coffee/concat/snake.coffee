class Snake extends Animal
  move: ->
    alert "Slithering..."
    super 5
    
sam = new Snake "Sammy the Python"
sam.move()