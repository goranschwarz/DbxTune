<?php


  die( 'Sorry the contact form does not work for the moment!<br>Instead mail me at: goran_schwarz@hotmail.com');

  /**
   * READ special config for ExecuteIT
   */
  $local_conf = '/etc/apache2/dbxtune.php.cfg';
  if( file_exists($local_conf)) {
    include( $local_conf );
  } else {
    die( 'Unable to load the "DbxTune Config" Library!, Instead mail: goran_schwarz@hotmail.com');
  }


  /**
  * Requires the "PHP Email Form" library
  * The "PHP Email Form" library is available only in the pro version of the template
  * The library should be uploaded to: vendor/php-email-form/php-email-form.php
  * For more info and help: https://bootstrapmade.com/php-email-form/
  */

  // Replace contact@example.com with your real receiving email address
//  $receiving_email_address = 'goran.schwarz@executeit.se';
  $receiving_email_address = 'goran_schwarz@hotmail.com';

  if( file_exists($php_email_form = '../assets/vendor/php-email-form/php-email-form.php' )) {
    include( $php_email_form );
  } else {
    die( 'Unable to load the "PHP Email Form" Library!');
  }

  $contact = new PHP_Email_Form;
  $contact->ajax = true;

  $contact->to = $receiving_email_address;
//  $contact->cc = array('goran.schwarz@executeit.se', 'inger.sorlen@executeit.se');
//  $contact->bcc = array('goran.schwarz@executeit.se', 'inger.sorlen@executeit.se');
  $contact->from_name = $_POST['name'];
  $contact->from_email = $_POST['email'];
  $contact->subject = '[DbxTune-web-contact] ' . $_POST['subject'];

  // SMTP
//  $contact->smtp = array(
//    'host' => 'smtp.office365.com',
//    'username' => $EXECUTEIT_EMAIL_USERNAME,
//    'password' => $EXECUTEIT_EMAIL_PASSWORD,
//    'port' => '587'
//  );
  $contact->smtp = array(
    'host' => 'smtp.google.com',
    'username' => 'dbxtune.alarms@gmail.com',
    'password' => 'REPLACE_THIS_WITH_SECRET_PASSWORD',
    'port' => '587'
  );

  $contact->add_message( $_POST['name'], 'From');
  $contact->add_message( $_POST['email'], 'Email');
  $contact->add_message( $_POST['phone'], 'Phone');
  $contact->add_message( $_POST['message'], 'Message', 10);

  echo $contact->send();
?>

