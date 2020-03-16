Feature: Login into todoist
    As an user I want to authenticate myself within todoist website

Scenario: Login failed
    Given I go to todoist home screen
    When I open the login screen
    And I fill a wrong email and password
    And I try to login
    Then I expect to not be able to login

Scenario: Login ok
    Given I go to todoist home screen
    When I open the login screen
    And I fill a correct email and password
    And I try to login
    Then I expect to be able to login