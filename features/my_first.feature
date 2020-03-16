Feature: Página principal

  Scenario: 1) As a user I touch Pagina principal option
    When I press the "MANTENERSE ANÓNIMO" button
    And I press "Página principal"
    And I wait for 3 seconds
    And I wait
    Then I see "principal"

  Scenario: 2) As a user I touch Todos los subreddits option
    When I press the "CERRAR" button
    And I press "Todos los subreddits"
    And I wait for 5 seconds
    And I wait
    Then I see "subreddits"

  Scenario: 3) As a user show Subreddits suscritos in home
    When I wait
    Then I see "Subreddits suscritos"

  Scenario: 4) As a user I touch Cuentas and show popup Cuentas de Reddit
    When I press the menu key
    And I press "Cuentas"
    Then I see "Cuentas de Reddit"
    
  Scenario: 5) As a user I touch Temas and show popup Tema
    When I press the menu key
    And I press "Temas"
    Then I see "Noche"

  Scenario: 6) As a user erase Subreddit from home
    When I press "art"
    And I wait
    And I press the menu key
    And I press "Fijar al menú principal"
    And I wait
    And I go back
    And I wait
    And I long press "/r/art"
    Then I press "Quitar del menú principal"

  Scenario: 7) As a user I touch Acerca de and press Novedades
    When I press the menu key
    And I press "Preferencias"
    And I press "Acerca de"
    And I press "Novedades"
    Then I see "1.9.10"

  Scenario: 8) As a user I long press aww Subreddit and press Fijar al menú principal
    When I long press "aww"
    And I wait
    Then I press "Fijar al menú principal"

  
