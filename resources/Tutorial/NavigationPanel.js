const template = document.createElement('template');

template.innerHTML = `<div class="sidenav">
                              <a href="TutorialPage.html">About</a>
                              <a href="DataCollectionPage.html">Data Collection</a>
                              <a href="DatabaseManagement.html">Database Management</a>
                              <a href="#">Charts</a>
                          </div>`;

document.body.appendChild(template.content);
