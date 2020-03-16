var { Given } = require('cucumber');
var { When } = require('cucumber');
var { Then } = require('cucumber');
var { expect } = require('chai');

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

///////////////////////////////

//Given('I go to todoist home screen', () => {
//  browser.url('/');
//});

//When('I open the login screen', () => {
//  $('.W9ktc').$$('li')[0].$('a').click();
//});

When('I fill a correct email and password', () => {
  var mailInput = $('input[name="email"]');
  mailInput.click();
  mailInput.setValue('ja.pereza@uniandes.edu.co');

  var passwordInput = $('input[name="password"]');
  passwordInput.click();
  passwordInput.setValue('uniandes');
});

//When('I try to login', () => {
//  $('.submit_btn').click();
//});

Then('I expect to be able to login', () => {
  browser.$('Inbox').isDisplayed();
});

/////////////////////////////

//Given('I go to todoist home screen', () => {
//  browser.url('/');
//});

//When('I open the login screen', () => {
//  $('.W9ktc').$$('li')[0].$('a').click();
//});

//When('I fill a correct email and password', () => {
//  var mailInput = $('input[name="email"]');
//  mailInput.click();
//  mailInput.setValue('ja.pereza@uniandes.edu.co');

//  var passwordInput = $('input[name="password"]');
//  passwordInput.click();
//  passwordInput.setValue('uniandes');
//});

//When('I try to login', () => {
//  $('.submit_btn').click();
//});

When('I click to add project', () => {
  var button = $('button[data-track="navigation|projects_quick_add"]');
  browser.waitUntil(() => button.isClickable());
  button.click();
});

When('I fill project name', () => {

  var nameInput = $('input[name="name"]');
  nameInput.click();
  nameInput.setValue('PruebaAUTOProyecto')
});

When('I try to create project', () => {
  $('.ist_button.ist_button_red').click();
});

Then('I expect to watch a new project', () => {
  $('PruebaAUTOProyecto').isDisplayed(5000);
});
