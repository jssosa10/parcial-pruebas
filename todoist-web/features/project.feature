Feature: Create project into todoist
    As an user I want to create a project in todoist website

Scenario: Login ok
    Given I go to todoist home screen
    When I open the login screen
    And I fill a correct email and password
    And I try to login
    And I click to add project
    And I fill project name
    And I try to create project    
    Then I expect to watch a new project

