(function () {
    /* step2: variables */
    let user_id = '23741962';
    let user_fullname = 'Ziyi Huang';
    let lng = -122.08;
    let lat = 37.38;

    /* step3: main function(entrance) */
    init();

    /* step4: define init function */
    function init() {
        // Register event listeners
        $('nearby-btn').addEventListener('click', loadNearbyItems);
        $('fav-btn').addEventListener('click', loadFavoriteItems);
        $('recommend-btn').addEventListener('click', loadRecommendedItems);


        const welcomeMsg = $('welcome-msg');
        welcomeMsg.innerHTML = 'Welcome, ' + user_fullname;

        // step 7
        initGeoLocation();

    }

    /* step5: create $ function */
    /**
     * A helper function that creates a DOM element <tag options...>
     */
    function $(tag, options) {
        if (!options) {
            return document.getElementById(tag);
        }
        const element = document.createElement(tag);

        for (const option in options) {
            if (options.hasOwnProperty(option)) {
                element[option] = options[option];
            }
        }
        return element;
    }

    /* step6: create AJAX helper function */
    /**
     * @param method - GET|POST|PUT|DELETE
     * @param url - API end point
     * @param data - data to send
     * @param callback - This the successful callback
     * @param errorHandler - This is the failed callback
     */
    function ajax(method, url, data, callback, errorHandler) {
        const xhr = new XMLHttpRequest();

        xhr.open(method, url, true);

        xhr.onload = function () {
            if (xhr.status === 200) {
                callback(xhr.responseText);
            } else {
                errorHandler();
            }
        };

        xhr.onerror = function () {
            console.error("The request couldn't be completed.");
            errorHandler();
        };

        if (data === null) {
            xhr.send();
        } else {
            xhr.setRequestHeader("Content-Type",
                "application/json;charset=utf-8");
            xhr.send(data);
        }
    }

    /** step 7: initGeoLocation function **/
    function initGeoLocation() {
        if (navigator.geolocation) {
            // step 8
            navigator.geolocation.getCurrentPosition(onPositionUpdated,
                onLoadPositionFailed, {
                    maximumAge: 60000
                });
            showLoadingMessage('Retrieving your location...');
        } else {
            // step 9
            onLoadPositionFailed();
        }
    }

    /** step 8: onPositionUpdated function **/
    function onPositionUpdated(position) {
        lat = position.coords.latitude;
        lng = position.coords.longitude;

        // step 11
        loadNearbyItems();
    }

    /** step 9: onPositionUpdated function **/
    function onLoadPositionFailed() {
        console.warn('navigator.geolocation is not available');

        //step 10
        getLocationFromIP();
    }

    /** step 10: getLocationFromIP function **/
    function getLocationFromIP() {
        // Get location from http://ipinfo.io/json
        const url = 'https://ipinfo.io/json';
        const req = null;
        ajax('GET', url, req, function (res) {
            const result = JSON.parse(res);
            if ('loc' in result) {
                const loc = result.loc.split(',');
                lat = loc[0];
                lng = loc[1];
            } else {
                console.warn('Getting location by IP failed.');
            }
            // step 11
            loadNearbyItems();
        });
    }

    /** step 11: loadNearbyItems function **/
    /**
     * API #1 Load the nearby items API end point: [GET]
     * /Jupiter/search?user_id=1111&lat=37.38&lon=-122.08
     */
    function loadNearbyItems() {
        console.log('loadNearbyItems');
        // step 12
        activeBtn('nearby-btn');

        // The request parameters
        const url = './search';
        const params = 'user_id=' + user_id + '&lat=' + lat + '&lon=' + lng;
        const req = JSON.stringify({});

        // step 13
        // display loading message
        showLoadingMessage('Loading nearby items...');

        // make AJAX call
        ajax('GET', url + '?' + params, req,
            // successful callback
            function (res) {
                const items = JSON.parse(res);
                if (!items || items.length === 0) {
                    // step 14
                    showWarningMessage('No nearby item.');
                } else {
                    // step 16
                    listItems(items);
                }
            },
            // failed callback
            function () {
                // step 15
                showErrorMessage('Cannot load nearby items.');
            });
    }

    /**
     * API #2 Load the favorite items API end point: [GET]
     * /Jupiter/history?user_id=23741962
     */
    function loadFavoriteItems() {
        // console.log('loadNearbyItems');
        // step 12
        activeBtn('fav-btn');

        // The request parameters
        const url = './history';
        const params = 'user_id=' + user_id;
        const req = JSON.stringify({});

        // step 13
        // display loading message
        showLoadingMessage('Loading favorite items...');

        // make AJAX call
        ajax('GET', url + '?' + params, req,
            // successful callback
            function (res) {
                const items = JSON.parse(res);
                if (!items || items.length === 0) {
                    // step 14
                    showWarningMessage('No favorite item.');
                } else {
                    // step 16
                    listItems(items);
                }
            },
            // failed callback
            function () {
                // step 15
                showErrorMessage('Cannot load favorite items.');
            });
    }

    /**
     * API #3 Load the recommended items API end point: [GET]
     * /Jupiter/recommendation?user_id=23741962
     */
    function loadRecommendedItems() {
        // console.log('loadNearbyItems');
        // step 12
        activeBtn('recommend-btn');

        // The request parameters
        const url = './recommendation';
        const params = 'user_id=' + user_id + '&lat=' + lat + '&lon=' + lng;
        const req = JSON.stringify({});

        // step 13
        // display loading message
        showLoadingMessage('Loading recommended items...');

        // make AJAX call
        ajax('GET', url + '?' + params, req,
            // successful callback
            function (res) {
                const items = JSON.parse(res);
                if (!items || items.length === 0) {
                    // step 14
                    showWarningMessage('No recommended item.');
                } else {
                    // step 16
                    listItems(items);
                }
            },
            // failed callback
            function () {
                // step 15
                showErrorMessage('Cannot load recommended items.');
            });
    }

    /**
     * API #4 Toggle favorite (or visited) items
     *
     * @param item_id -
     *            The item business id
     *
     * API end point: [POST]/[DELETE] /Jupiter/history request json data: {
     * user_id: 1111, visited: [a_list_of_business_ids] }
     */
    function changeFavoriteItem(item_id) {
        // Check whether this item has been visited or not
        const li = $('item-' + item_id);
        const favIcon = $('fav-icon-' + item_id);
        const favorite = li.dataset.favorite !== 'true';

        // The request parameters
        const url = './history';
        const req = JSON.stringify({
            user_id: user_id,
            favorite: [item_id]
        });
        const method = favorite ? 'POST' : 'DELETE';

        ajax(method, url, req,
            // successful callback
            function (res) {
                const result = JSON.parse(res);
                if (result.result === 'SUCCESS') {
                    li.dataset.favorite = favorite.toString();
                    favIcon.className = favorite ? 'fa fa-heart' : 'fa fa-heart-o';
                }
            });
    }

    /** step 12: activeBtn function **/
    /**
     * A helper function that makes a navigation button active
     *
     * @param btnId - The id of the navigation button
     */
    function activeBtn(btnId) {
        const btns = document.getElementsByClassName('main-nav-btn');

        // deactivate all navigation buttons
        for (let i = 0; i < btns.length; i++) {
            btns[i].className = btns[i].className.replace(/\bactive\b/, '');
        }

        // active the one that has id = btnId
        const btn = $(btnId);
        btn.className += ' active';
    }

    /** step 13: showLoadingMessage function **/
    function showLoadingMessage(msg) {
        const itemList = $('item-list');
        itemList.innerHTML = '<p class="notice"><i class="fa fa-spinner fa-spin"></i> '
            + msg + '</p>';
    }

    /** step 14: showWarningMessage function **/
    function showWarningMessage(msg) {
        const itemList = $('item-list');
        itemList.innerHTML = '<p class="notice"><i class="fa fa-exclamation-triangle"></i> '
            + msg + '</p>';
    }

    /** step15: showErrorMessage function **/
    function showErrorMessage(msg) {
        const itemList = $('item-list');
        itemList.innerHTML = '<p class="notice"><i class="fa fa-exclamation-circle"></i> '
            + msg + '</p>';
    }

    /** step16: listItems function **/
    /**
     * @param items - An array of item JSON objects
     */
    function listItems(items) {
        // Clear the current results
        const itemList = $('item-list');
        itemList.innerHTML = '';

        for (let i = 0; i < items.length; i++) {
            // step 17
            addItem(itemList, items[i]);
        }
    }

    /** step17: addItem function **/
    /**
     * Add item to the list
     * @param itemList - The <ul id="item-list"> tag
     * @param item - The item data (JSON object)
     */
    function addItem(itemList, item) {
        const item_id = item.item_id;

        // create the <li> tag and specify the id and class attributes
        const li = $('li', {
            id: 'item-' + item_id,
            className: 'item'
        });

        // set the data attribute
        li.dataset.item_id = item_id;
        li.dataset.favorite = item.favorite;

        // item image
        if (item.image_url) {
            li.appendChild($('img', {
                src: item.image_url
            }));
        } else {
            li.appendChild($('img', {
                src: 'https://assets-cdn.github.com/images/modules/logos_page/GitHub-Mark.png'
            }))
        }

        // item header
        const itemHeader = $('div', {
            className: 'item-header'
        });

        // title
        const title = $('a', {
            href: item.url,
            target: '_blank',
            className: 'item-name'
        });
        title.innerHTML = item.name;
        itemHeader.appendChild(title);

        // favorite link
        const favLink = $('p', {
            className: 'fav-link'
        });

        favLink.onclick = function () {
            changeFavoriteItem(item_id);
        };

        const favIcon = $('i', {
            id: 'fav-icon-' + item_id,
            className: item.favorite ? 'fa fa-heart' : 'fa fa-heart-o'
        });
        favLink.appendChild(favIcon);
        itemHeader.appendChild(favLink);

        li.appendChild(itemHeader);

        // item body
        const itemBody = $('div', {
            className: 'item-body'
        });

        // category
        const category = $('p', {
            className: 'item-category'
        });
        category.innerHTML = 'Category: ' + item.categories.join(', ');
        itemBody.appendChild(category);

        // address
        const address = $('p', {
            className: 'item-address'
        });

        address.innerHTML = item.address.replace(/,/g, '<br/>').replace(/"/g,
            '');
        itemBody.appendChild(address);

        li.appendChild(itemBody);

        itemList.appendChild(li);
    }
})()