<?
  require_once('gui.php');
?>
<html>
<head>
  <title>WikiVerifier -- Legend</title>
<style type="text/css">
  h1 {
    font-weight: bold;
    font-size: 12pt;
    padding:10px 0px 5px 0px;
    text-align:center
  }
  h2 {
    font-weight: normal;
    font-size: 10pt;
    padding:10px 0px 5px 0px
  }
table.wv_table {
        border-width: 0px;
        border-spacing: 0px;
        border-style: solid;
        border-color: black;
        border-collapse: collapse;
        background-color: white;
        padding:3px;
}
table.wv_table th {
        border-width: 1px;
        border-style: solid;
        background-color: #eeeeee;
        padding:3px;
        -moz-border-radius: 0px;
}
table.wv_table td {
        border-width: 1px;
        border-style: solid;
        background-color: white;
        padding:3px;
        -moz-border-radius: 0px;
}
</style>
</head>
<body>
  <table border=0>
  <tr>
    <td width='100%'>
      <h1>WikiVerifier -- Legend</h1>
    </td>
    <td>
      <img src='images/logo_small.png'>
    </td>
  </tr>
  </table>

  <table class='wv_table' width='80%' align='center'>
  <thead>
    <th>Icon</th>
    <th>Meaning</th>
    <th>Where it's shown</th>
  </thead>
  <tbody>
    <tr>
      <th><img src='images/blue.png'></th>
      <td>
        <b>Validation irrelevant</b>: This modification was performed by a
        registered, active WikiVerifier user. As such, it's irrelevant whether
        the modification has been verified or not, because that user can verify
        it at any time. Disabling a user's WikiVerifier account will instantly
        make all his/her changes abide by "normal" rules, and this icon is no
        longer shown for that user's modifications, even retroactively.
      </td>
      <td>Recent changes, article history pages, user contributions pages</td>
    </tr>
    <tr>
      <th><img src='images/navyblue.png'></th>
      <td>
        <b>Validated by trust</b>: This modification was performed by a trusted
        user. You can indicate the users you trust by using the controls in the
        popup window which is shown when you click on a small icon associated
        with one of those user's edits. Users trusted by one WikiVerifier user
        are instantly trusted by all WikiVerifier users, so make sure you know
        who to trust.
      </td>
      <td>Recent changes, article history pages, user contributions pages</td>
    </tr>
    <tr>
      <th><img src='images/red_navyblue.png'></th>
      <td>
        <b>Validated by trust</b>: Same as above, but this is a user who has
        recently been added to the trusted users list (less than 31 days ago).
      </td>
      <td>Recent changes, article history pages, user contributions pages</td>
    </tr>
    <tr>
      <th><img src='images/black.png'></th>
      <td>
        <b>Validated by community</b>: This modification was performed by a
        registered robot. It is assumed that registered robots have the
        &quot;blessing&quot; of the community, and their changes shouldn't need
        to be checked for vandalisms.
      </td>
      <td>Recent changes, article history pages, user contributions pages</td>
    </tr>
    <tr>
      <th><img src='images/red.png'></th>
      <td><b>Not validated</b>: This difference hasn't been seen by anyone.</td>
      <td>Recent changes, article history pages, user contributions pages</td>
    </tr>
    <tr>
      <th><img src='images/pink_red.png'></th>
      <td>
        <b>Not validated</b>: This difference hasn't been seen by anyone.
        Additionally, nobody has even seen the fact that it has taken place
        (if you reload, this will change to the icon above, since you've now
        seen that the article was modified). This is useful to see how active
        other people are (lots of these in the Recent Changes page means no
        other WikiVerifier user has been around for a while).
      </td>
      <td>Recent changes, article history pages, user contributions pages</td>
    </tr>
    <tr>
      <th><img src='images/red_exclamation.png'></th>
      <td>
        <b>Not validated</b>: This difference has been seen by at least one
        person, but hasn't been validated by any of the people who saw it.
      </td>
      <td>Recent changes, article history pages, user contributions pages</td>
    </tr>
    <tr>
      <th><img src='images/green_myself.png'></th>
      <td>
        <b>Validated</b>: The current user is the only person who has validated
        this difference. Other people might have seen it too, but if they did,
        none of them has validated it.
      </td>
      <td>Recent changes, article history pages, user contributions pages</td>
    </tr>
    <tr>
      <th><img src='images/green_others.png'></th>
      <td>
        <b>Validated</b>: Other people have validated this difference.
        The current user might have seen it too, but didn't validate it.
      </td>
      <td>Recent changes, article history pages, user contributions pages</td>
    </tr>
    <tr>
      <th><img src='images/green_both.png'></th>
      <td>
        <b>Validated</b>: This difference has been validated by both the
        current user and at least one other person.
      </td>
      <td>Recent changes, article history pages, user contributions pages</td>
    </tr>
    <tr>
      <th><img src='images/grey.png'></th>
      <td><b>Undetermined</b>: WikiVerifier was unable to identify this difference, and can determine no information about its validity.</td>
      <td>Recent changes, article history pages, user contributions pages</td>
    </tr>
    <tr>
      <th><img src='images/blue_large.png'></th>
      <td>
        <b>Validation irrelevant</b>: This modification was performed by a
        registered, active WikiVerifier user. As such, it's irrelevant whether
        the modification has been verified or not, because that user can verify
        it at any time. Disabling a user's WikiVerifier account will instantly
        make all his/her changes abide by "normal" rules, and this icon is no
        longer shown for that user's modifications, even retroactively.</td>
      <td>Page showing the differences between two versions of an article</td>
    </tr>
    <tr>
      <th><img src='images/navyblue_large.png'></th>
      <td>
        <b>Validated by trust</b>: This modification was performed by a trusted
        user. You can indicate the users you trust by using the controls in the
        popup window which is shown when you click on a small icon associated
        with one of those user's edits. Users trusted by one WikiVerifier user
        are instantly trusted by all WikiVerifier users, so make sure you know
        who to trust.
      </td>
      <td>Page showing the differences between two versions of an article</td>
    </tr>
    <tr>
      <th><img src='images/red_navyblue_large.png'></th>
      <td>
        <b>Validated by trust</b>: Same as above, but this is a user who has
        recently been added to the trusted users list (less than 31 days ago).
      </td>
      <td>Page showing the differences between two versions of an article</td>
    </tr>
    <tr>
      <th><img src='images/black_large.png'></th>
      <td>
        <b>Validated by community</b>: This modification was performed by a
        registered robot. It is assumed that registered robots have the
        &quot;blessing&quot; of the community, and their changes shouldn't need
        to be checked for vandalisms.
      </td>
      <td>Page showing the differences between two versions of an article</td>
    </tr>
    <tr>
      <th><img src='images/red_green_large.png'></th>
      <td>
        <b>Validated</b>: This difference hadn't been validated by anyone
        before you. Since you're looking at it, you're validating it right now.
      </td>
      <td>Page showing the differences between two versions of an article</td>
    </tr>
    <tr>
      <th><img src='images/green_large.png'></th>
      <td>
        <b>Validated</b>: This difference has already been validated by someone
        (either yourself or other users). Your validation will be added to the
        existing ones, if you haven't already validated it before.
      </td>
      <td>Page showing the differences between two versions of an article</td>
    </tr>
    <tr>
      <th><img src='images/red_exclamation_large.png'></th>
      <td>
        <b>Not validated</b>: This difference had been validated, but yours was
        the only standing validation; since you have withdrawn your validation
        by clicking the icon, this difference is not validated anymore.
      </td>
      <td>
        Page showing the differences between two versions of an article, after
        clicking the icon
      </td>
    </tr>
    <tr>
      <th><img src='images/green_black_large.png'></th>
      <td>
        <b>Validated</b>: Both yourself and other people had validated this
        difference. Now you have withdrawn your validation by clicking the
        icon, but the validation still stands due to the others.
      </td>
      <td>
        Page showing the differences between two versions of an article, after
        clicking the icon
      </td>
    </tr>
    <tr>
      <th><img src='images/grey_large.png'></th>
      <td>
        <b>Undetermined</b>: WikiVerifier was unable to identify this
        difference, and can determine no information about its validity.
      </td>
      <td>
        Page showing the differences between two versions of an article,
        irrespective of whether you clicked the icon.
      </td>
    </tr>
    <tr>
      <th><img src='images/unknown_block_large.png'></th>
      <td>
        <b>Undetermined block</b>: WikiVerifier was unable to identify the
        block you're currently into, and can determine no information about its
        validity.
      </td>
      <td>
        Page showing the differences between two versions of an article, after
        choosing to validate the current block.
      </td>
    </tr>
    <tr>
      <th><img src='images/no_block_large.png'></th>
      <td>
        <b>No block</b>: There is no block, there's a single change.
        You have chosen to validate an entire block, but there is neither
        a previous nor a subsequent modification made by this same user,
        at least in the proximity of this modification.
      </td>
      <td>
        Page showing the differences between two versions of an article,
        after choosing to validate the current block.
      </td>
    </tr>
    <tr>
      <th><img src='images/validated_block_large.png'></th>
      <td>
        <b>Validated block</b>: The current block of modifications has been
        validated.
      </td>
      <td>
        Page showing the differences between two versions of an article,
        after choosing to validate the current block.
      </td>
    </tr>
  </tbody>
  </table>
</body>
</html>
