var {Given} = require('cucumber');
var {When} = require('cucumber');
var {Then} = require('cucumber');
var {expect} = require('chai');

Given('I go to todoist home screen', () => {
  browser.url('/');
});

When('I open the login screen', () => {
  $('.W9ktc').$$('li')[0].$('a').click();
});

When('I fill a wrong email and password', () => {
  var mailInput = $('input[name="email"]');
  mailInput.click();
  mailInput.setValue('wrongemail@example.com');

  var passwordInput = $('input[name="password"]');
  passwordInput.click();
  passwordInput.setValue('123467891');
});

When('I try to login', () => {
  $('.submit_btn').click();
});

Then('I expect to not be able to login', () => {
  $('.error_msg').waitForDisplayed(5000);
});