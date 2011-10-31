class Horse extends Animal
  move: ->
    alert "Galloping..."
    super 45


tom = new Horse "Tommy the Palomino"

tom.move()