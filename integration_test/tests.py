# -*- coding: utf-8 -*-
'''Parking Proxy tests
'''
import time
import json
import unittest
import logging

from decimal import Decimal
from datetime import (datetime,
                      timedelta)
from appstore_cli import AppStoreClient


STORE_URL = 'http://localhost:30380/api/v1/'

USER = {'_id': 'f6c5626e-23c6-46d0-a0c0-2c83269fc254'}
INAPP_TMPL = {"product_id": "io.drewnoff.subscriptions.%s",
              "title": "Simple InApp",
              "author": "DrewNoff",
              "autorenewable": True,
              "period": 10,
              "description": "Test InApp"}

class TestAppStoreInApps(unittest.TestCase):

    @classmethod
    def print_settings(cls):
        '''Displays tests configuration
        '''
        print 'proxy_url:           %s' % cls.proxy_url

    def _crete_user_fixture(self, user):
        '''Registers user.
        '''
        resp = self.iface.register_user(user)
        # TODO some assert

    def _subscribe(self, user, period, subscription_plan):
        '''Creates InApp autorenewable subscription with @period sec period.
        and subscribes @user according to @subscription_plan.
        '''
        product_id = INAPP_TMPL['product_id'] % str(time.time())
        inapp = dict(INAPP_TMPL)
        inapp['product_id'] = product_id
        inapp['period'] = period
        self.iface.create_inapp(inapp)
        self.iface.subscribe(user['_id'], product_id, subscription_plan)
        return product_id


    def setUp(self):
        '''Should restore only orig receipt at the beginning of subscription.
        '''
        super(TestAppStoreInApps, self).setUp()
        self.store_url = STORE_URL
        self.iface = AppStoreClient(self.store_url)
        self.logger = logging.getLogger('store_test')
        self._crete_user_fixture(USER)

    def test_restore_orig_receipt(self):
        '''Should restore one receipt at the beginning
        '''
        log = self.logger
        user = USER
        log.debug('===test_restore_orig_receipt===')
        subs_plan = [{'value': 'Trial$'},
                     {'value': 'Paid$'},
                     {'value': 'Paid$'},
                     {'idle': 1.4}]
        product_id = self._subscribe(user, 10, subs_plan)
        receipts = self.iface.restore(user['_id'], product_id)['receipts']
        log.debug("resotred receipts: %r", receipts)
        self.assertEqual(len(receipts), 1)

    def test_verify_orig_receipt(self):
        '''Should verify orig receipt.
        Last receipt should be equal current receipt.
        '''
        log = self.logger
        user = USER
        log.debug('===test_verify_orig_receipt===')
        subs_plan = [{'value': 'Trial$'},
                     {'value': 'Paid$'},
                     {'value': 'Paid$'},
                     {'idle': 1.4}]
        product_id = self._subscribe(user, 10, subs_plan)
        receipt = self.iface.restore(user['_id'], product_id)['receipts'][0]
        log.debug("orig receipt: %r", receipt)
        resp = self.iface.verify(receipt)
        self.assertEqual(resp['status'], 0)
        self.assertEqual(resp['receipt']['transaction_id'],
                         receipt['transaction_id'])
        self.assertEqual(resp['receipt']['transaction_id'],
                         resp['latest_receipt_info']['transaction_id'])

    def test_verify_prolonged_subscription(self):
        '''Should retrieve next receipt after subscription prolongation.
        '''
        log = self.logger
        user = USER
        log.debug('===test_verify_prolonged_subscription===')
        subs_plan = [{'value': 'Trial$'},
                     {'value': 'Paid$'},
                     {'value': 'Paid$'},
                     {'idle': 1.4}]
        product_id = self._subscribe(user, 3, subs_plan)
        orig_receipt = self.iface.restore(user['_id'], product_id)['receipts'][-1]
        time.sleep(3)
        resp = self.iface.verify(orig_receipt)
        self.assertEqual(resp['status'], 0)
        latest_receipt = resp['latest_receipt_info']
        receipt = resp['receipt']
        self.assertEqual(receipt['transaction_id'],
                         orig_receipt['transaction_id'])
        self.assertNotEqual(receipt['transaction_id'],
                            latest_receipt['transaction_id'])
        self.assertEqual(receipt['original_transaction_id'],
                         orig_receipt['transaction_id'])


def configure_logging():
    logging.basicConfig(
            format='%(asctime)s %(thread)d %(levelname)s %(name)s %(message)s')
    root_logger = logging.getLogger('')
    root_logger.setLevel(logging.DEBUG)
    fh = logging.FileHandler('store_test.log')
    formatter = logging.Formatter(
        '%(asctime)s %(thread)d %(levelname)s %(name)s %(message)s')
    fh.setLevel(logging.DEBUG)
    fh.setFormatter(formatter)
    root_logger.addHandler(fh)


if __name__ == '__main__':
    configure_logging()
    unittest.main()
